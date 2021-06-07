# --- !Ups
create table if not exists Products
(
    "id"          integer not null primary key autoincrement,
    "name"        varchar not null,
    "description" text    not null,
    "price"       double  not null,
    "quantity"    integer not null,
    "category_id" integer not null,
    "active"      bool    not null,
    foreign key (category_id) references Categories(id)
);

create table if not exists Promotions
(
    "id"          integer not null primary key autoincrement,
    "product_id"  integer not null,
    "discount"    double  not null,
    "type"        varchar not null,
    "active"      bool    not null,
    foreign key (product_id) references Products(id),
    constraint unique_product_id unique ("product_id")
);

create table if not exists Vouchers
(
    "id"          integer not null primary key autoincrement,
    "name"        varchar not null,
    "discount"    double  not null,
    "type"        varchar not null,
    "active"      bool    not null,
    constraint unique_number unique ("name")
);

create table if not exists Categories
(
    "id"          integer not null primary key autoincrement,
    "name"        varchar not null,
    "description" text    not null,
    "active"      bool    not null
);

create table if not exists BonusCards
(
    "id"          integer not null primary key autoincrement,
    "customer_id" long    not null,
    "number"      varchar not null,
    "status"      varchar not null,
    "active"      bool    not null,
    constraint unique_number unique ("number")
);

insert into Categories(name, description, active) values("Meat", "Meat related products", True);

create table if not exists Customers
(
    "id"          integer not null primary key autoincrement,
    "email"       varchar not null,
    "password"    varchar not null,
    "firstName"   integer not null,
    "lastName"    integer not null,
    "active"      bool    not null
);

create table if not exists Users
(
    "id"          integer not null primary key autoincrement,
    "email"       varchar not null,
    "password"    varchar not null,
    "firstName"   integer not null,
    "lastName"    integer not null,
    "active"      bool    not null
);

create table if not exists UserLogs
(
    "id"          integer not null primary key autoincrement,
    "email"       varchar not null,
    "providerId"  varchar,
    "providerKey" varchar
);

create table if not exists AuthToken
(
    "id"          integer not null primary key autoincrement,
    "userId"      integer not null
);

create table if not exists "passwordInfo"
(
    "id"          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "providerId"  VARCHAR NOT NULL,
    "providerKey" VARCHAR NOT NULL,
    "hasher"      VARCHAR NOT NULL,
    "password"    VARCHAR NOT NULL,
    "salt"        VARCHAR
);

create table if not exists "OAuthInfo"
(
    "id"          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "providerId"  VARCHAR NOT NULL,
    "providerKey" VARCHAR NOT NULL,
    "accessToken" VARCHAR NOT NULL,
    "tokenType"   VARCHAR,
    "expiresIn"   INTEGER
);

create table if not exists Carts
(
    "id"          integer not null primary key autoincrement,
    "customer_id" integer not null,
    "product_id"  integer not null,
    "quantity"    integer not null,
    "order_id"    integer,
    foreign key (customer_id) references Customers(id),
    foreign key (product_id) references Products(id),
    foreign key (order_id) references Orders(id)
);

create table if not exists Transactions
(
    "id"          integer not null primary key autoincrement,
    "order_id"    integer not null,
    "final_price" integer not null,
    "status"      integer not null,
    "active"      bool    not null,
    foreign key (order_id) references Orders(id)
);

create table if not exists Orders
(
    "id"          integer not null primary key autoincrement,
    "customer_id" integer not null,
    "price"       integer not null,
    "promotions_discount"       double  not null,
    "voucher_discount"         double  not null,
    "state"       varchar not null,
    "active"      bool    not null,
    foreign key (customer_id) references Customers(id)
);

# --- !Downs
drop table Products;
drop table Categories;
drop table BonusCards;
drop table Customers;
drop table Carts;
drop table Users;
drop table Orders;
drop table Vouchers;
drop table Promotions;
drop table Transactions;