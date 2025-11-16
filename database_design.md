# TPC Payroll Management System - Simplified Database Design

## Entity Relationship Diagram (ERD)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                USERS TABLE                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK │ user_id          │ INT(11)        │ AUTO_INCREMENT │ NOT NULL │ PRIMARY KEY │
│    │ username         │ VARCHAR(50)    │                │ NOT NULL │ UNIQUE      │
│    │ password_hash    │ VARCHAR(255)   │                │ NOT NULL │             │
│    │ full_name        │ VARCHAR(100)   │                │ NOT NULL │             │
│    │ email            │ VARCHAR(100)   │                │ NOT NULL │ UNIQUE      │
│    │ role             │ VARCHAR(50)    │                │ NOT NULL │             │
│    │ status           │ ENUM           │                │ NOT NULL │ DEFAULT     │
│    │                  │ ('Active',     │                │          │ 'Active'    │
│    │                  │  'Inactive',   │                │          │             │
│    │                  │  'Suspended')  │                │          │             │
│    │ last_login       │ VARCHAR(50)    │                │ NULL     │             │
│    │ created_date     │ VARCHAR(50)    │                │ NOT NULL │             │
└─────────────────────────────────────────────────────────────────────────────────┘
                                                              │
                                                              │ N:1
                                                              ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                               ROLES TABLE                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK │ role_id          │ INT(11)        │ AUTO_INCREMENT │ NOT NULL │ PRIMARY KEY │
│    │ role_name        │ VARCHAR(50)    │                │ NOT NULL │ UNIQUE      │
│    │ description      │ TEXT           │                │ NULL     │             │
│    │ user_count       │ INT(11)        │                │ NOT NULL │ DEFAULT 0   │
│    │ status           │ ENUM           │                │ NOT NULL │ DEFAULT     │
│    │                  │ ('Active',     │                │          │ 'Active'    │
│    │                  │  'Inactive')   │                │          │             │
│    │ created_date     │ VARCHAR(50)    │                │ NOT NULL │             │
└─────────────────────────────────────────────────────────────────────────────────┘
                                                              │
                                                              │ 1:N
                                                              ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            ROLE_PERMISSIONS TABLE                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK │ role_permission_id│ INT(11)       │ AUTO_INCREMENT │ NOT NULL │ PRIMARY KEY │
│ FK │ role_id          │ INT(11)        │                │ NOT NULL │ FOREIGN KEY │
│ FK │ permission_id    │ INT(11)        │                │ NOT NULL │ FOREIGN KEY │
│    │ granted          │ BOOLEAN        │                │ NOT NULL │ DEFAULT TRUE│
│    │ created_date     │ DATETIME       │                │ NOT NULL │             │
└─────────────────────────────────────────────────────────────────────────────────┘
                                                              │
                                                              │ N:1
                                                              ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            PERMISSIONS TABLE                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK │ permission_id    │ INT(11)        │ AUTO_INCREMENT │ NOT NULL │ PRIMARY KEY │
│    │ permission_name  │ VARCHAR(100)   │                │ NOT NULL │ UNIQUE      │
│    │ module_name      │ VARCHAR(50)    │                │ NOT NULL │             │
│    │ action_name      │ VARCHAR(50)    │                │ NOT NULL │             │
│    │ description      │ TEXT           │                │ NULL     │             │
│    │ status           │ ENUM           │                │ NOT NULL │ DEFAULT     │
│    │                  │ ('Active',     │                │          │ 'Active'    │
│    │                  │  'Inactive')   │                │          │             │
│    │ created_date     │ DATETIME       │                │ NOT NULL │             │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Relationships:
- **Users** N:1 **Roles** (Many users can have one role - simplified)
- **Roles** 1:N **Role_Permissions** (One role can have multiple permissions)
- **Permissions** 1:N **Role_Permissions** (One permission can be assigned to multiple roles)

## Key Features:
1. **Simplified User System**: Users have one role directly (no many-to-many)
2. **Granular Permissions**: Fine-grained permission control per module/action
3. **Basic User Info**: Only essential fields matching your data tables
4. **Role-Based Access**: Direct role assignment to users
