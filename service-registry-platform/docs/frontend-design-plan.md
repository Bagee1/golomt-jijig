# Frontend Design Plan

Огноо: 2026-07-06

Энэ document нь frontend хийхээс өмнө хэрэгтэй screen/slide, component, өнгө design-ийн суурь жагсаалт юм.

## 1. Design direction

Төслийн frontend нь банкны байгууллагын internal admin portal шиг харагдана.

Гол vibe:

- Цэвэр, ажил хэрэгч, enterprise dashboard.
- Хэт гоёлын landing page биш, шууд ашигладаг tool.
- Table, filter, form, detail view дээр төвлөрсөн.
- Security/compliance мэдээллийг хурдан scan хийх боломжтой.
- Mobile дээр эвдрэхгүй responsive боловч үндсэн хэрэглээ desktop.

## 2. Color palette

Primary өнгө нь банк/enterprise мэдрэмжтэй teal-green байна. Гэхдээ бүх UI нэг өнгийн болохоос сэргийлж blue, amber, red accent ашиглана.

| Token | Color | Ашиглах хэсэг |
| --- | --- | --- |
| `primary.main` | `#006B5B` | Primary button, active nav, main links |
| `primary.dark` | `#004D42` | Hover, selected state |
| `primary.light` | `#D8F3EC` | Light badges, selected row background |
| `secondary.main` | `#2457C5` | Info link, secondary action, chart color |
| `accent.main` | `#F5B700` | Warning highlight, attention indicator |
| `success.main` | `#16845B` | UP, PASS, active status |
| `warning.main` | `#B7791F` | WARNING, partial compliance |
| `error.main` | `#C73535` | DOWN, FAIL, validation error |
| `background.default` | `#F6F8FB` | App background |
| `background.paper` | `#FFFFFF` | Table/form/card surface |
| `sidebar.bg` | `#17202A` | Left navigation |
| `text.primary` | `#111827` | Main text |
| `text.secondary` | `#5B6472` | Supporting text |
| `border.default` | `#D8DEE8` | Table, input, divider |

Status color mapping:

| Status | Color | UI |
| --- | --- | --- |
| `ACTIVE`, `UP`, `PASS` | `#16845B` | Green chip |
| `WARNING`, `UNKNOWN`, `NOT_CHECKED` | `#B7791F` | Amber chip |
| `DOWN`, `FAIL`, `INACTIVE` | `#C73535` | Red chip |
| `DEV`, `TEST`, `UAT`, `PROD` | `#2457C5`, `#7C3AED`, `#B7791F`, `#006B5B` | Environment chips |

## 3. Typography and spacing

Typography:

- Font: `Inter` эсвэл Material UI default `Roboto`.
- Page title: 24-28px, semibold.
- Section title: 18-20px, semibold.
- Table text: 13-14px.
- Helper text: 12-13px.

Spacing:

- App shell gutter: 24px desktop, 16px tablet/mobile.
- Card radius: 8px.
- Input height: 40px.
- Table row height: 56px.
- Toolbar height: 56px.

## 4. Slide / screen list

Эдгээрийг Figma frame эсвэл presentation slide болгон зурж болно.

| # | Slide / screen | Зорилго |
| ---: | --- | --- |
| 1 | Login | Admin user нэвтрэх flow |
| 2 | App shell | Sidebar, topbar, user menu, content layout |
| 3 | Dashboard overview | Нийт system, active/inactive, average security score |
| 4 | Systems list | Table, search, filter, pagination |
| 5 | Systems empty/loading/error states | UX state-үүд |
| 6 | Create system form | PDF-ийн бүх талбартай бүртгэлийн form |
| 7 | Edit system form | Existing system засах flow |
| 8 | System detail overview | Metadata, URLs, owner/developer info |
| 9 | Related systems tab | Холбоотой системүүдийн map/list |
| 10 | Security checklist tab | Control list, result select, evidence, score |
| 11 | Audit log page | Action history table |
| 12 | Responsive mobile view | Sidebar collapse, table scroll, form stacking |
| 13 | Banking service demo preview | Дараагийн enterprise demo-д зориулсан placeholder |

## 5. Core layout components

| Component | Үүрэг |
| --- | --- |
| `AppShell` | Authenticated app-ийн үндсэн layout |
| `SidebarNav` | Dashboard, Systems, Audit, Settings nav |
| `TopBar` | Page context, user menu, logout |
| `PageHeader` | Title, subtitle, primary action |
| `BreadcrumbsBar` | Detail/form page navigation |
| `ContentContainer` | Max width, responsive padding |
| `ProtectedRoute` | Tokenгүй үед login руу redirect хийх |
| `RoleGate` | ADMIN/SECURITY role-specific UI hide/show |
| `UserMenu` | Current user, role, logout |

## 6. Shared UI components

| Component | Ашиглах газар |
| --- | --- |
| `LoadingState` | Page/table/form loading |
| `ErrorState` | API error display |
| `EmptyState` | Empty table/list |
| `ConfirmDialog` | Delete/disable confirmation |
| `ToastProvider` | Success/error notification |
| `StatusChip` | ACTIVE, INACTIVE, UNKNOWN, DOWN |
| `SystemTypeChip` | CARD, CORE, INTERNAL, DIGITAL |
| `EnvironmentChip` | DEV, TEST, UAT, PROD |
| `SecurityResultChip` | PASS, WARNING, FAIL, NOT_CHECKED |
| `SecurityScoreBadge` | 0-100 score badge |
| `SearchInput` | Keyword search |
| `FilterSelect` | Type/status/environment filter |
| `DataToolbar` | Search + filters + action buttons |
| `PaginationControls` | Page/size control |
| `DateDisplay` | Date formatting |
| `MoneyDisplay` | MNT formatting |
| `ExternalLinkButton` | Base URL, health URL, swagger URL, repo URL |

## 7. Auth components

| Component | Fields / behavior |
| --- | --- |
| `LoginPage` | Centered login layout |
| `LoginForm` | Username, password, submit |
| `PasswordField` | Show/hide password icon |
| `AuthErrorAlert` | Invalid login error |
| `AuthProvider` | Token, current user, login/logout state |

Login page design:

- Left side: product name and short enterprise registry label.
- Right side: login form.
- Mobile дээр нэг column.
- Default test credential hint-г зөвхөн dev mode-д харуулж болно.

## 8. Dashboard components

| Component | Data |
| --- | --- |
| `DashboardPage` | Dashboard route wrapper |
| `MetricCard` | Total systems, Active, Inactive, Average score |
| `SystemStatusSummary` | ACTIVE/INACTIVE/UNKNOWN/DOWN distribution |
| `SystemTypeDistribution` | CARD/CORE/INTERNAL/DIGITAL chart |
| `RecentAuditList` | Last audit actions |
| `LowSecuritySystemsTable` | Score багатай systems |
| `QuickActionsPanel` | New system, View audit, Security controls |

Эхний frontend-д dashboard backend API байхгүй бол:

- `GET /api/systems` response дээрээс total/count-г client талд түр тооцож болно.
- Recent audit-д `GET /api/audit-logs?size=5` ашиглана.
- Average security score-г эхний хувилбарт system detail дээр харуулаад dashboard дээр placeholder байж болно.

## 9. System list components

| Component | Үүрэг |
| --- | --- |
| `SystemsListPage` | Systems route |
| `SystemsTable` | Name, type, valuation, developer, inUse, status, actions |
| `SystemsFilterBar` | Keyword, type, developer, inUse, status |
| `SystemsTableRowActions` | View, edit, disable |
| `SystemStatusCell` | Status + in use display |
| `SystemLinksCell` | Swagger/health/repo quick links |
| `CreateSystemButton` | ADMIN only |

Table columns:

```text
Name
Type
Valuation MNT
Developer
Team
Environment
In use
Status
Created at
Actions
```

## 10. System form components

| Component | Fields |
| --- | --- |
| `SystemCreatePage` | Create route |
| `SystemEditPage` | Edit route |
| `SystemForm` | Shared create/edit form |
| `SystemIdentitySection` | systemKey, name, type |
| `SystemBusinessSection` | valuationMnt, description |
| `SystemDeveloperSection` | developerName, developerTeam |
| `SystemTimelineSection` | startDate, endDate |
| `SystemRuntimeSection` | environment, inUse, status |
| `SystemUrlSection` | baseUrl, healthUrl, swaggerUrl, repoUrl |
| `RelatedSystemsPicker` | relatedSystems multi-select |
| `FormActionBar` | Save, cancel |

Required fields:

```text
name
type
valuationMnt
```

Validation:

- `valuationMnt >= 0`.
- `startDate <= endDate`.
- URL fields valid if present.
- Self relation сонгохгүй.

## 11. System detail components

| Component | Үүрэг |
| --- | --- |
| `SystemDetailPage` | Detail route |
| `SystemDetailHeader` | Name, status, type, action buttons |
| `SystemOverviewTab` | Metadata, developer, valuation, dates |
| `SystemLinksPanel` | Base, health, swagger, repo links |
| `RelatedSystemsTab` | Relation table/cards |
| `SecurityChecklistTab` | Security result update |
| `SystemAuditTab` | тухайн system-ийн audit history, backend filter дараа нэмнэ |
| `SystemDangerZone` | Disable system, ADMIN only |

Tabs:

```text
Overview
Related systems
Security checklist
Audit
```

## 12. Security components

| Component | Үүрэг |
| --- | --- |
| `SecurityScorePanel` | Score, earned/total weight |
| `SecurityChecklistTable` | Control бүрийн result |
| `SecurityResultSelect` | PASS/WARNING/FAIL/NOT_CHECKED |
| `SecurityEvidenceField` | Evidence textarea |
| `SecuritySaveBar` | Save changes |
| `SecurityControlDescription` | Control title/description/standard ref |
| `SecuritySummaryChips` | pass/warning/fail/not checked counts |

Security tab columns:

```text
Control
Weight
Required
Automated
Result
Evidence
Checked at
```

## 13. Audit components

| Component | Үүрэг |
| --- | --- |
| `AuditLogPage` | Audit route |
| `AuditLogTable` | Action history |
| `AuditActionChip` | LOGIN_SUCCESS, SYSTEM_CREATED гэх мэт |
| `AuditActorCell` | actor user info |
| `AuditTargetCell` | target type/id |
| `AuditMetadataDrawer` | metadata JSON дэлгэрэнгүй |

Audit table columns:

```text
Created at
Action
Actor
Target
Message
Metadata
```

## 14. API and state modules

| File / module | Үүрэг |
| --- | --- |
| `api/httpClient.ts` | Axios instance, base URL, token interceptor |
| `api/authApi.ts` | login, me |
| `api/systemsApi.ts` | system CRUD/search |
| `api/securityApi.ts` | controls, checks, score |
| `api/auditApi.ts` | audit logs |
| `types/auth.ts` | User, LoginResponse |
| `types/system.ts` | SystemResponse, create/update request |
| `types/security.ts` | Security controls/checks/score |
| `types/audit.ts` | AuditLogResponse |
| `hooks/useAuth.ts` | Auth state |
| `hooks/useDebouncedValue.ts` | Search debounce |
| `utils/format.ts` | date/money/status formatting |

## 15. Route map

| Route | Page |
| --- | --- |
| `/login` | `LoginPage` |
| `/` | Redirect to `/dashboard` |
| `/dashboard` | `DashboardPage` |
| `/systems` | `SystemsListPage` |
| `/systems/new` | `SystemCreatePage` |
| `/systems/:id` | `SystemDetailPage` |
| `/systems/:id/edit` | `SystemEditPage` |
| `/audit-logs` | `AuditLogPage` |

## 16. Icon plan

Lucide icon ашиглавал:

| UI | Icon |
| --- | --- |
| Dashboard | `LayoutDashboard` |
| Systems | `Server` |
| Security | `ShieldCheck` |
| Audit | `History` |
| Create | `Plus` |
| Edit | `Pencil` |
| Disable | `CircleOff` |
| Search | `Search` |
| Filter | `SlidersHorizontal` |
| Swagger/API | `BookOpen` |
| Health | `Activity` |
| Repo | `GitBranch` |
| External link | `ExternalLink` |
| Logout | `LogOut` |

## 17. Build order

Frontend-г хийх дараалал:

1. Vite React TypeScript project үүсгэх.
2. Material UI, React Router, Axios, lucide-react суулгах.
3. Theme tokens болон AppShell хийх.
4. AuthProvider + Login flow хийх.
5. Systems list + filters хийх.
6. System create/edit form хийх.
7. System detail tabs хийх.
8. Security checklist tab хийх.
9. Audit log page хийх.
10. Dashboard page-г backend боломжид тааруулж угсрах.

## 18. Design checklist

Зурах эсвэл code хийхдээ шалгах зүйл:

- Login page шууд ойлгомжтой.
- Sidebar дээр active route тод харагдана.
- Table filter нэг мөрөнд багтахгүй бол responsive wrap хийнэ.
- Button text mobile дээр тасрахгүй.
- Status chip өнгө утгатайгаа зөрөхгүй.
- Form section-ууд card дотор хэт олон nested card болохгүй.
- Detail page дээр PDF-ийн бүх field харагдана.
- Security score 0-100 утгаараа шууд ойлгогдоно.
- Audit log action-ууд scan хийхэд амар байна.
- Empty/loading/error state бүх гол page дээр байна.
