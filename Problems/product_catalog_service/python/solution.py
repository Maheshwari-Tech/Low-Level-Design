"""One-hour Python core for an authoritative, versioned product catalog."""

from __future__ import annotations

from dataclasses import dataclass, field, replace
from datetime import datetime, timezone
from decimal import Decimal
from enum import Enum
from threading import RLock
from types import MappingProxyType
from typing import Callable, Mapping, Sequence, Union

AttributeValue = Union[str, int, Decimal, bool]


class AttributeType(str, Enum):
    TEXT = "text"
    INTEGER = "integer"
    DECIMAL = "decimal"
    BOOLEAN = "boolean"
    ENUM = "enum"


class AttributeScope(str, Enum):
    PRODUCT = "product"
    VARIANT = "variant"


class ProductStatus(str, Enum):
    DRAFT = "draft"
    ACTIVE = "active"
    ARCHIVED = "archived"


class ChangeType(str, Enum):
    CREATED = "created"
    UPDATED = "updated"
    PUBLISHED = "published"
    ARCHIVED = "archived"
    REACTIVATED = "reactivated"
    SCHEMA_INVALIDATED = "schema_invalidated"


class CatalogError(RuntimeError):
    pass


class VersionConflict(CatalogError):
    pass


class DuplicateSku(CatalogError):
    pass


@dataclass(frozen=True, order=True)
class ValidationIssue:
    path: str
    message: str


class ProductValidationError(CatalogError):
    def __init__(self, product_id: str, issues: Sequence[ValidationIssue]) -> None:
        super().__init__(f"product {product_id} has {len(issues)} validation issue(s)")
        self.issues = tuple(sorted(issues))


@dataclass(frozen=True)
class AttributeDefinition:
    key: str
    label: str
    value_type: AttributeType
    scope: AttributeScope
    required: bool = False
    enum_values: frozenset[str] = field(default_factory=frozenset)

    def __post_init__(self) -> None:
        if not self.key.strip() or not self.label.strip():
            raise ValueError("attribute key and label must not be blank")
        if (self.value_type is AttributeType.ENUM) != bool(self.enum_values):
            raise ValueError("exactly enum attributes must declare enum_values")

    def violation(self, value: AttributeValue) -> str | None:
        valid_type = {
            AttributeType.TEXT: type(value) is str,
            AttributeType.INTEGER: type(value) is int,
            AttributeType.DECIMAL: type(value) is Decimal,
            AttributeType.BOOLEAN: type(value) is bool,
            AttributeType.ENUM: type(value) is str,
        }[self.value_type]
        if not valid_type:
            return f"expected {self.value_type.value}"
        if self.value_type is AttributeType.ENUM and value not in self.enum_values:
            return f"must be one of {sorted(self.enum_values)}"
        return None


def _freeze(values: Mapping[str, AttributeValue]) -> Mapping[str, AttributeValue]:
    return MappingProxyType(dict(values))


@dataclass(frozen=True)
class Category:
    category_id: str
    name: str
    parent_id: str | None
    definitions: Mapping[str, AttributeDefinition]
    version: int

    def __post_init__(self) -> None:
        if not self.category_id.strip() or not self.name.strip():
            raise ValueError("category_id and name must not be blank")
        if self.version <= 0:
            raise ValueError("category version must be positive")
        object.__setattr__(self, "definitions", MappingProxyType(dict(self.definitions)))


@dataclass(frozen=True)
class Variant:
    variant_id: str
    sku: str
    attributes: Mapping[str, AttributeValue]

    def __post_init__(self) -> None:
        if not self.variant_id.strip() or not self.sku.strip():
            raise ValueError("variant_id and sku must not be blank")
        object.__setattr__(self, "attributes", _freeze(self.attributes))


@dataclass(frozen=True)
class Product:
    product_id: str
    name: str
    description: str
    brand: str
    category_id: str
    attributes: Mapping[str, AttributeValue]
    variants: tuple[Variant, ...]
    media: tuple[str, ...]
    status: ProductStatus
    version: int
    created_at: datetime
    changed_at: datetime
    actor: str

    def __post_init__(self) -> None:
        if not self.product_id.strip() or not self.category_id.strip() or not self.actor.strip():
            raise ValueError("product_id, category_id, and actor must not be blank")
        if self.version <= 0:
            raise ValueError("product version must be positive")
        object.__setattr__(self, "attributes", _freeze(self.attributes))
        object.__setattr__(self, "variants", tuple(self.variants))
        object.__setattr__(self, "media", tuple(self.media))


@dataclass(frozen=True)
class ProductVersion:
    change_type: ChangeType
    snapshot: Product


@dataclass(frozen=True)
class SchemaImpact:
    product_id: str
    previous_status: ProductStatus
    before: tuple[ValidationIssue, ...]
    after: tuple[ValidationIssue, ...]


class ProductRepository:
    """A DB adapter would replace this lock with version and unique-SKU constraints."""

    def __init__(self) -> None:
        self._products: dict[str, Product] = {}
        self._sku_owner: dict[str, str] = {}
        self._history: dict[str, list[ProductVersion]] = {}
        self._lock = RLock()

    def get(self, product_id: str) -> Product:
        with self._lock:
            if product_id not in self._products:
                raise CatalogError(f"unknown product: {product_id}")
            return self._products[product_id]

    def get_by_sku(self, sku: str) -> Product:
        with self._lock:
            owner = self._sku_owner.get(sku)
            if owner is None:
                raise CatalogError(f"unknown SKU: {sku}")
            return self._products[owner]

    def all(self) -> tuple[Product, ...]:
        with self._lock:
            return tuple(self._products.values())

    def history(self, product_id: str) -> tuple[ProductVersion, ...]:
        with self._lock:
            return tuple(self._history.get(product_id, ()))

    def save(
        self,
        product: Product,
        change_type: ChangeType,
        expected_version: int | None,
    ) -> Product:
        with self._lock:
            current = self._products.get(product.product_id)
            if expected_version is None:
                if current is not None:
                    raise CatalogError(f"duplicate product: {product.product_id}")
                if product.version != 1:
                    raise VersionConflict("a new product must start at version 1")
            elif current is None or current.version != expected_version:
                actual = current.version if current else "missing"
                raise VersionConflict(f"expected {expected_version}, actual {actual}")
            else:
                if product.version != expected_version + 1:
                    raise VersionConflict(
                        "a saved revision must increase the version by exactly one"
                    )
                old = {variant.variant_id: variant.sku for variant in current.variants}
                new = {variant.variant_id: variant.sku for variant in product.variants}
                if any(new.get(variant_id) != sku for variant_id, sku in old.items()):
                    raise CatalogError("existing variants cannot be removed or re-keyed")

            skus = [variant.sku for variant in product.variants]
            if len(skus) != len(set(skus)):
                raise DuplicateSku("a product cannot contain the same SKU twice")
            for sku in skus:
                owner = self._sku_owner.get(sku)
                if owner is not None and owner != product.product_id:
                    raise DuplicateSku(f"SKU {sku} is permanently owned by {owner}")
            for sku in skus:
                self._sku_owner[sku] = product.product_id
            self._products[product.product_id] = product
            self._history.setdefault(product.product_id, []).append(
                ProductVersion(change_type, product)
            )
            return product


class ProductCatalogService:
    def __init__(
        self,
        repository: ProductRepository,
        next_product_id: Callable[[], str],
        clock: Callable[[], datetime] = lambda: datetime.now(timezone.utc),
    ) -> None:
        self._repository = repository
        self._next_product_id = next_product_id
        self._clock = clock
        self._categories: dict[str, Category] = {}
        self._lock = RLock()

    def create_category(
        self,
        category_id: str,
        name: str,
        definitions: Sequence[AttributeDefinition],
        actor: str,
        parent_id: str | None = None,
    ) -> Category:
        if not actor.strip():
            raise ValueError("actor must not be blank")
        del actor  # an ORM/outbox adapter would persist the taxonomy audit record
        with self._lock:
            if category_id in self._categories:
                raise CatalogError(f"duplicate category: {category_id}")
            if parent_id is not None:
                self._category(parent_id)
            mapped = {definition.key: definition for definition in definitions}
            if len(mapped) != len(definitions):
                raise ValueError("duplicate attribute definition")
            category = Category(category_id, name, parent_id, mapped, 1)
            self._categories[category_id] = category
            return category

    def effective_schema(self, category_id: str) -> Mapping[str, AttributeDefinition]:
        with self._lock:
            lineage: list[Category] = []
            seen: set[str] = set()
            category = self._category(category_id)
            while True:
                if category.category_id in seen:
                    raise CatalogError("category hierarchy contains a cycle")
                seen.add(category.category_id)
                lineage.append(category)
                if category.parent_id is None:
                    break
                category = self._category(category.parent_id)
            schema: dict[str, AttributeDefinition] = {}
            for category in reversed(lineage):
                schema.update(category.definitions)
            return MappingProxyType(schema)

    def create_product(
        self,
        *,
        name: str,
        description: str,
        brand: str,
        category_id: str,
        attributes: Mapping[str, AttributeValue],
        variants: Sequence[Variant],
        media: Sequence[str],
        actor: str,
    ) -> Product:
        with self._lock:
            self._category(category_id)
            now = self._clock()
            product = Product(
                self._next_product_id(), name, description, brand, category_id,
                attributes, tuple(variants), tuple(media), ProductStatus.DRAFT,
                1, now, now, actor,
            )
            self._require_valid(product, complete=False)
            return self._repository.save(product, ChangeType.CREATED, None)

    def update_product(
        self,
        product_id: str,
        expected_version: int,
        actor: str,
        **changes: object,
    ) -> Product:
        with self._lock:
            current = self._repository.get(product_id)
            self._version(current, expected_version)
            if current.status is ProductStatus.ARCHIVED:
                raise CatalogError("an archived product cannot be edited")
            forbidden = {"product_id", "status", "version", "created_at", "changed_at", "actor"}
            if forbidden.intersection(changes):
                raise ValueError("identity, lifecycle, and audit fields cannot be patched")
            revised = replace(
                current, **changes, version=current.version + 1,
                changed_at=self._clock(), actor=actor,
            )
            self._category(revised.category_id)
            self._require_valid(revised, revised.status is ProductStatus.ACTIVE)
            return self._repository.save(revised, ChangeType.UPDATED, expected_version)

    def publish(self, product_id: str, expected_version: int, actor: str) -> Product:
        return self._transition(
            product_id, expected_version, ProductStatus.DRAFT, ProductStatus.ACTIVE,
            ChangeType.PUBLISHED, actor, validate=True,
        )

    def archive(self, product_id: str, expected_version: int, actor: str) -> Product:
        with self._lock:
            current = self._repository.get(product_id)
            if current.status is ProductStatus.ARCHIVED:
                raise CatalogError("product is already archived")
            self._version(current, expected_version)
            return self._save_transition(
                current, ProductStatus.ARCHIVED, ChangeType.ARCHIVED, actor, False
            )

    def reactivate(self, product_id: str, expected_version: int, actor: str) -> Product:
        return self._transition(
            product_id, expected_version, ProductStatus.ARCHIVED, ProductStatus.ACTIVE,
            ChangeType.REACTIVATED, actor, validate=True,
        )

    def change_category_schema(
        self,
        category_id: str,
        expected_version: int,
        definitions: Sequence[AttributeDefinition],
        actor: str,
    ) -> tuple[SchemaImpact, ...]:
        with self._lock:
            category = self._category(category_id)
            if category.version != expected_version:
                raise VersionConflict("category schema version conflict")
            mapped = {definition.key: definition for definition in definitions}
            if len(mapped) != len(definitions):
                raise ValueError("duplicate attribute definition")
            products = tuple(
                product for product in self._repository.all()
                if product.category_id in self._descendants(category_id)
            )
            before = {p.product_id: self._validate(p, complete=True) for p in products}
            self._categories[category_id] = replace(
                category, definitions=mapped, version=category.version + 1
            )
            impacts: list[SchemaImpact] = []
            for product in products:
                after = self._validate(product, complete=True)
                invalid_active = product.status is ProductStatus.ACTIVE and bool(after)
                if before[product.product_id] != after or invalid_active:
                    impacts.append(
                        SchemaImpact(product.product_id, product.status, before[product.product_id], after)
                    )
                if invalid_active:
                    self._save_transition(
                        product, ProductStatus.DRAFT, ChangeType.SCHEMA_INVALIDATED,
                        actor, False,
                    )
            return tuple(impacts)

    def get(self, product_id: str) -> Product:
        return self._repository.get(product_id)

    def get_by_sku(self, sku: str) -> Product:
        return self._repository.get_by_sku(sku)

    def history(self, product_id: str) -> tuple[ProductVersion, ...]:
        return self._repository.history(product_id)

    def active_catalog(self) -> tuple[Product, ...]:
        return tuple(sorted(
            (p for p in self._repository.all() if p.status is ProductStatus.ACTIVE),
            key=lambda product: product.product_id,
        ))

    def _transition(
        self,
        product_id: str,
        expected_version: int,
        required: ProductStatus,
        target: ProductStatus,
        change_type: ChangeType,
        actor: str,
        validate: bool,
    ) -> Product:
        with self._lock:
            current = self._repository.get(product_id)
            self._version(current, expected_version)
            if current.status is not required:
                raise CatalogError(f"cannot transition {current.status.value} to {target.value}")
            return self._save_transition(current, target, change_type, actor, validate)

    def _save_transition(
        self,
        current: Product,
        target: ProductStatus,
        change_type: ChangeType,
        actor: str,
        validate: bool,
    ) -> Product:
        changed = replace(
            current, status=target, version=current.version + 1,
            changed_at=self._clock(), actor=actor,
        )
        if validate:
            self._require_valid(changed, complete=True)
        return self._repository.save(changed, change_type, current.version)

    def _require_valid(self, product: Product, complete: bool) -> None:
        issues = self._validate(product, complete)
        if issues:
            raise ProductValidationError(product.product_id, issues)

    def _validate(self, product: Product, complete: bool) -> tuple[ValidationIssue, ...]:
        schema = self.effective_schema(product.category_id)
        product_defs = {k: d for k, d in schema.items() if d.scope is AttributeScope.PRODUCT}
        variant_defs = {k: d for k, d in schema.items() if d.scope is AttributeScope.VARIANT}
        issues: list[ValidationIssue] = []
        self._validate_values("attributes", product.attributes, product_defs, complete, issues)
        if complete:
            for path, value in (
                ("name", product.name),
                ("description", product.description),
                ("brand", product.brand),
            ):
                if not value.strip():
                    issues.append(ValidationIssue(path, "is required for publication"))
        if complete and not product.variants:
            issues.append(ValidationIssue("variants", "at least one variant is required"))

        ids: set[str] = set()
        skus: set[str] = set()
        combinations: set[tuple[tuple[str, str, str], ...]] = set()
        for index, variant in enumerate(product.variants):
            path = f"variants[{index}]"
            if variant.variant_id in ids:
                issues.append(ValidationIssue(f"{path}.variant_id", "is duplicated"))
            if variant.sku in skus:
                issues.append(ValidationIssue(f"{path}.sku", "is duplicated"))
            ids.add(variant.variant_id)
            skus.add(variant.sku)
            self._validate_values(
                f"{path}.attributes", variant.attributes, variant_defs, complete, issues
            )
            combination = tuple(sorted(
                (key, type(value).__name__, str(value))
                for key, value in variant.attributes.items() if key in variant_defs
            ))
            if variant_defs and combination in combinations:
                issues.append(ValidationIssue(f"{path}.attributes", "duplicates another variant"))
            combinations.add(combination)
        return tuple(sorted(issues))

    @staticmethod
    def _validate_values(
        path: str,
        values: Mapping[str, AttributeValue],
        definitions: Mapping[str, AttributeDefinition],
        complete: bool,
        issues: list[ValidationIssue],
    ) -> None:
        for key, value in values.items():
            definition = definitions.get(key)
            if definition is None:
                issues.append(ValidationIssue(f"{path}.{key}", "is not defined by the schema"))
            elif violation := definition.violation(value):
                issues.append(ValidationIssue(f"{path}.{key}", violation))
        if complete:
            issues.extend(
                ValidationIssue(f"{path}.{definition.key}", "required attribute is missing")
                for definition in definitions.values()
                if definition.required and definition.key not in values
            )

    def _descendants(self, root_id: str) -> set[str]:
        result = {self._category(root_id).category_id}
        while True:
            expanded = result | {
                category.category_id for category in self._categories.values()
                if category.parent_id in result
            }
            if expanded == result:
                return result
            result = expanded

    def _category(self, category_id: str) -> Category:
        if category_id not in self._categories:
            raise CatalogError(f"unknown category: {category_id}")
        return self._categories[category_id]

    @staticmethod
    def _version(product: Product, expected: int) -> None:
        if product.version != expected:
            raise VersionConflict(f"expected {expected}, actual {product.version}")
