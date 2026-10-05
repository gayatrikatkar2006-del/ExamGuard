# ExamGuard

**A rule-based examination seating and invigilation allocation system for colleges.**

ExamGuard replaces the manual work of the exam cell. The admin schedules exams and halls, and the system builds the seating plan and the invigilation roster from a short list of written rules. Students see their own seat, faculty see their own duties, and nobody can be double-booked.

Live site (Render): `https://<your-service-name>.onrender.com` (replace with your link)

## Technology stack (same as BookMyLab)

| Experiment | Where it appears in ExamGuard |
|---|---|
| 1 HTML5 + CSS3 | Semantic pages, tables and forms, styled by one hand-written `styles.css` (grid layout, dark mode, print styles for the seating plan) |
| 2, 7 REST + Spring Boot | 21 REST endpoints for halls, exams, allocation, dashboards and users |
| 3, 4 React, JSX, Hooks, Context | Auth and Toast contexts, a custom `useForm` hook with validation on every form, React Router |
| 5, 6 Kotlin | Entities as Kotlin classes, a custom exception hierarchy, `filter / map / groupBy / reduce` in the dashboard, and a **Kotlin Lab** page showing live inheritance, null safety and try/catch |
| 8 JWT | Login and register, BCrypt password hashing, role-based access (ADMIN, FACULTY, STUDENT) |
| 9 Deployment | `Dockerfile` and `render.yaml` for Render, or run as a single jar locally |

Database: embedded H2 (no installation). Vite is used only as the React build tool.

## The rules

**Seating**
1. Students are placed in roll-number order, hall by hall (halls in alphabetical order).
2. Halls under maintenance are never used.
3. A seat already used by another exam in the same time slot is skipped.
4. Alternate-seat rule: two students of the same exam never sit side by side.
5. If anyone cannot be seated, nothing is saved and the admin is told how many are left.

**Invigilation**
1. One invigilator for every 30 students in a hall (at least one per hall).
2. Faculty never invigilate an exam of their own department.
3. Nobody is placed in two exams that overlap in time.
4. Nobody gets more than 6 duties in total.
5. The faculty member with the fewest duties so far is chosen first (fair workload).
6. The first invigilator in each hall is the chief.

**Scheduling:** an exam is rejected if it overlaps in time with another exam that shares students (same department, or any exam marked `ALL`).

Exam status: `DRAFT` (no seats) then `SEATED` (seats, no invigilators) then `READY`.

## Database (5 tables)

| Table | Purpose |
|---|---|
| `users` | admin, faculty and students (role, department, roll number) |
| `halls` | name, building, rows x seats per row, status |
| `exams` | code, subject, department (or `ALL`), date, start, end |
| `seat_allocations` | exam, hall, student, row, column |
| `invigilations` | exam, hall, faculty, CHIEF or ASSISTANT |

## Project structure

```
ExamGuard/
  backend/    Kotlin + Spring Boot (pom.xml, src/main/kotlin/com/examguard/...)
  frontend/   React + Vite (src/pages, components, context, hooks, styles.css)
  postman/    ExamGuard.postman_collection.json (35 requests with tests)
  Dockerfile  render.yaml
```

## Run it locally

You need JDK 21, Maven 3.9+ and Node.js 20+ (check with `java -version`, `mvn -v`, `node -v`).

```bash
cd frontend
npm install
npm run build          # writes the website into backend/src/main/resources/static

cd ../backend
mvn clean package -DskipTests
java -jar target/examguard.jar
```

Open http://localhost:8080. Health check: http://localhost:8080/api/health

On Windows PowerShell use `cd ..\backend` and `java -jar target\examguard.jar`. If port 8080 is busy: `$env:PORT=9090; java -jar target\examguard.jar`.

Development mode (two terminals): `mvn spring-boot:run` in `backend`, then `npm run dev` in `frontend` and open http://localhost:5173.

### Demo logins

| Role | Email | Password |
|---|---|---|
| Admin | admin@examguard.com | admin123 |
| Faculty (AIML) | isha@faculty.com | faculty123 |
| Student (CSE001) | cse001@student.com | student123 |

Other faculty: priya, arun (CSE), kavita, sameer (ECE), neha, vikram (MECH), rohit (AIML), all `@faculty.com`.
Other students: `cse001` to `cse024`, `ece001` to `ece018`, `mech001` to `mech012`, `aiml001` to `aiml012`, all `@student.com`.

Seed data: 66 students, 8 faculty, 4 usable halls (Hall A 5x6, B 6x6, C 5x8, D 5x6) plus a Seminar Hall under maintenance, and 5 exams starting a week from the first run. To reset everything, stop the app and delete `backend/data`.

## Test plan

1. **Public pages:** Home shows the rules and a sample hall. Kotlin Lab shows live Kotlin output.
2. **Validation:** on Log in press the button with empty fields, then enter `abc` as the email. Register a student with mismatched passwords.
3. **Seating (admin):** log in as admin, open *Exams and allocation*, click *Allocate seats* on **CS301**. 24 students go into Hall A and Hall B.
4. **Shared slot:** click *Allocate seats* on **EC302** (same date and time). Open its plan: hatched seats are CS301, and the ECE students fill the gaps. Nobody sits next to a student of their own exam.
5. **Invigilators:** click *Allocate invigilators* on CS301, then EC302. Check that no CSE faculty is on CS301, no ECE faculty is on EC302, and nobody is on both.
6. **Not enough seats:** open *Halls*, set Hall D to MAINTENANCE, then allocate seats for **MA101** (all 66 students). You get "Not enough seats: 13 of 66 students could not be seated". Set Hall D back to AVAILABLE and try again: it succeeds.
7. **Clash rule:** schedule a new CSE exam on the same date and time as CS301. You get a clash error (409).
8. **Locked hall:** try to delete or put into maintenance a hall that has seats. You are told to reset the allocation first.
9. **Student:** log in as `cse001@student.com`. *My exam seats* shows the hall, the seat label and a highlighted seat on the hall map.
10. **Faculty:** log in as a faculty member. *My invigilation duties* lists duties, and you can open the seating plan of each exam. Use *Print seating plan* for a clean printout.
11. **Roles:** as a student or faculty, try to open `/#/admin`. You are redirected. The API returns 403 for admin-only calls.
12. **Postman:** import the collection and run folder `1. Auth` first (it saves the three tokens), then the rest. Expected 401, 403, 404 and 409 cases are tested.

## REST API (21 endpoints)

| Method and path | Who | Purpose |
|---|---|---|
| GET `/api/health` | public | health check |
| GET `/api/kotlin-demo` | public | live Kotlin Lab output |
| POST `/api/auth/register`, `/api/auth/login` | public | create account, get JWT |
| GET `/api/auth/me` | any user | current user |
| GET `/api/halls` | any user | list halls |
| POST `/api/halls`, PUT/DELETE `/api/halls/{id}` | admin | hall CRUD |
| GET `/api/exams`, GET `/api/exams/{id}` | admin, faculty | list and view exams |
| POST `/api/exams`, DELETE `/api/exams/{id}` | admin | schedule or delete an exam |
| POST `/api/exams/{id}/allocate-seats` | admin | run the seating rules |
| POST `/api/exams/{id}/allocate-invigilators` | admin | run the invigilation rules |
| DELETE `/api/exams/{id}/allocation` | admin | reset seats and invigilators |
| GET `/api/exams/{id}/seating` | admin, faculty | seating plan with hall maps |
| GET `/api/me/seats` | student | my seats |
| GET `/api/me/duties` | faculty | my duties |
| GET `/api/users?role=` | admin | list people |
| GET `/api/dashboard/admin` | admin | dashboard statistics |

## Deploy on Render (Experiment 9)

1. Push the project to a new GitHub repository (`git init`, `git add .`, `git commit`, `git push`).
2. On render.com choose **New, Blueprint**, pick the repository and click **Apply**. Render reads `render.yaml`.
3. The `Dockerfile` builds the React app and the Spring Boot jar into one image. `JWT_SECRET` is generated by Render, and the health check is `/api/health`.
4. Open the `onrender.com` link and log in with the demo accounts.

On the free plan the service sleeps when idle and the H2 data (including allocations) resets on restart, then the demo data is recreated. For permanent data, switch to a managed database later.

## Troubleshooting

| Problem | Fix |
|---|---|
| Blank page | Run `npm run build` in `frontend`, then rebuild the backend |
| "Cannot reach the server" | The jar is not running |
| Logged out suddenly | Tokens last 24 hours, log in again |
| Demo logins stopped working | Stop the app, delete `backend/data`, start again |
| Maven or Kotlin compile error | Send me the full error text and I will fix it |

## Honest status

The code was written and checked by reading and by simulating the seating rules on the seed data (24 + 18 students sharing Hall A and B with no side-by-side seats, 66 students filling four halls, 13 left over when Hall D is in maintenance). It has **not been compiled or run yet**, because the build environment blocked Maven Central and npm. Please run the build once and report any error.
