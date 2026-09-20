from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from itertools import count
from threading import Barrier
import unittest

from solution import (
    AttributeDefinition,
    AttributeScope,
    AttributeType,
    ChangeType,
    DuplicateSku,
    ProductCatalogService,
    ProductRepository,
    ProductStatus,
    ProductValidationError,
    Variant,
    VersionConflict,
)


class ProductCatalogTests(unittest.TestCase):
    def setUp(self) -> None:
        sequence = count(1)
        self.catalog = ProductCatalogService(
            ProductRepository(),
            lambda: f"product-{next(sequence)}",
            lambda: datetime(2026, 8, 11, tzinfo=timezone.utc),
        )
        self.material = AttributeDefinition(
            "material", "Material", AttributeType.TEXT, AttributeScope.PRODUCT, True
        )
        self.fit = AttributeDefinition(
            "fit", "Fit", AttributeType.ENUM, AttributeScope.PRODUCT, True,
            frozenset({"REGULAR", "SLIM"}),
        )
        self.color = AttributeDefinition(
            "color", "Color", AttributeType.ENUM, AttributeScope.VARIANT, True,
            frozenset({"RED", "BLUE"}),
        )
        self.size = AttributeDefinition(
            "size", "Size", AttributeType.ENUM, AttributeScope.VARIANT, True,
            frozenset({"S", "M"}),
        )
        self.catalog.create_category(
            "apparel", "Apparel", [self.material, self.color, self.size], "taxonomy"
        )
        self.catalog.create_category(
            "shirts", "Shirts", [self.fit], "taxonomy", parent_id="apparel"
        )

    @staticmethod
    def variants(sku: str = "SHIRT-RED-S") -> list[Variant]:
        return [Variant("red-small", sku, {"color": "RED", "size": "S"})]

    def create_valid(self):
        return self.catalog.create_product(
            name="Oxford Shirt",
            description="Durable cotton shirt",
            brand="Northwind",
            category_id="shirts",
            attributes={"material": "cotton", "fit": "REGULAR"},
            variants=self.variants(),
            media=["https://cdn.example.test/oxford.jpg"],
            actor="editor",
        )

    def test_publish_lookup_and_permanent_sku_ownership(self) -> None:
        draft = self.create_valid()
        active = self.catalog.publish(draft.product_id, draft.version, "publisher")
        self.assertEqual(active.status, ProductStatus.ACTIVE)
        self.assertEqual(self.catalog.get_by_sku("SHIRT-RED-S"), active)

        archived = self.catalog.archive(active.product_id, active.version, "admin")
        self.assertEqual(self.catalog.get_by_sku("SHIRT-RED-S"), archived)
        with self.assertRaises(DuplicateSku):
            self.catalog.create_product(
                name="Collision",
                description="Another product",
                brand="Contoso",
                category_id="shirts",
                attributes={"material": "linen", "fit": "SLIM"},
                variants=self.variants(),
                media=[],
                actor="editor",
            )

    def test_typed_validation_and_incomplete_draft_cannot_publish(self) -> None:
        with self.assertRaises(ProductValidationError) as invalid_type:
            self.catalog.create_product(
                name="Wrong type",
                description="Fit must be an enum string",
                brand="Northwind",
                category_id="shirts",
                attributes={"material": "cotton", "fit": 7},
                variants=self.variants("SHIRT-TYPE-ERROR"),
                media=[],
                actor="editor",
            )
        self.assertIn(
            "attributes.fit", {issue.path for issue in invalid_type.exception.issues}
        )

        draft = self.catalog.create_product(
            name="Incomplete",
            description="Missing material",
            brand="Northwind",
            category_id="shirts",
            attributes={"fit": "SLIM"},
            variants=[Variant("blue-m", "SHIRT-BLUE-M", {"color": "BLUE", "size": "M"})],
            media=[],
            actor="editor",
        )
        with self.assertRaises(ProductValidationError) as raised:
            self.catalog.publish(draft.product_id, draft.version, "publisher")
        self.assertIn("attributes.material", {issue.path for issue in raised.exception.issues})

    def test_two_stale_editors_produce_one_conflict(self) -> None:
        active = self.catalog.publish(
            self.create_valid().product_id, 1, "publisher"
        )
        barrier = Barrier(2)

        def update(description: str) -> str:
            barrier.wait()
            try:
                self.catalog.update_product(
                    active.product_id,
                    active.version,
                    description=description,
                    actor=description,
                )
                return "success"
            except VersionConflict:
                return "conflict"

        with ThreadPoolExecutor(max_workers=2) as executor:
            outcomes = list(executor.map(update, ("editor-a", "editor-b")))
        self.assertCountEqual(outcomes, ["success", "conflict"])

    def test_schema_change_demotes_invalid_active_product(self) -> None:
        active = self.catalog.publish(
            self.create_valid().product_id, 1, "publisher"
        )
        with self.assertRaises(ValueError):
            self.catalog.change_category_schema(
                "apparel", 1, [self.material, self.material], "taxonomy"
            )
        care = AttributeDefinition(
            "care", "Care", AttributeType.TEXT, AttributeScope.PRODUCT, True
        )
        impacts = self.catalog.change_category_schema(
            "apparel", 1, [self.material, self.color, self.size, care], "taxonomy"
        )
        self.assertEqual([impact.product_id for impact in impacts], [active.product_id])
        invalidated = self.catalog.get(active.product_id)
        self.assertEqual(invalidated.status, ProductStatus.DRAFT)
        self.assertEqual(invalidated.version, active.version + 1)
        self.assertNotIn(invalidated, self.catalog.active_catalog())
        self.assertEqual(
            self.catalog.history(active.product_id)[-1].change_type,
            ChangeType.SCHEMA_INVALIDATED,
        )


if __name__ == "__main__":
    unittest.main(verbosity=2)
