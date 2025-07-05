# UI Overhaul Requirements for Home Screen - Calculator App

## 📌 Objective
Improve the usability, appearance, and layout of the home screen. Focus on decluttering, better spacing, intuitive grid layout for category buttons, and easier access to the calculator via FAB.

---

## ✅ Required UI Changes

### 1. 🧹 Reduce Vertical Space Below "Recent Calculations"
- Reduce the vertical gap between the label `Recent Calculations` and the recent history scroll list.
- Change `android:layout_marginTop` of the label section to `4dp` or less.
- Make the list feel attached to the heading by removing excessive spacing.

---

### 2. 📦 Increase Size of Recent History Cards
- Modify recent history cards inside `recent_history_container`:
    - Set fixed size to around `160dp width` and `120dp height`.
    - Only **2 recent entries** should be fully visible on screen at once.
    - Remaining entries should scroll **horizontally**.
- Ensure cards have:
    - Rounded corners
    - Slight shadow (`cardElevation`)
    - Margin between items for visual separation

---

### 3. 🔢 Convert Category Buttons into 3x3 Grid
- Replace all `FlexboxLayout` sections (like `algebra_buttons`, `geometry_buttons`, etc.) with `GridLayout`.
- Grid Layout Specifications:
    - `columnCount="3"`
    - Buttons should use:
      ```xml
      android:layout_width="0dp"
      android:layout_height="wrap_content"
      android:layout_columnWeight="1"
      ```
    - Ensure even spacing and auto-fitting button height/width depending on device width.

---

### 4. ✍️ Allow Button Text to Wrap (2 Lines)
- All category buttons should support up to **2 lines of text**:
    - Use:
      ```xml
      android:maxLines="2"
      android:ellipsize="end"
      android:gravity="center"
      android:textAlignment="center"
      ```
    - Keep text `textSize="14sp"` or adapt for visibility.

---

### 5. ➕ Add Floating Calculator Button (FAB)
- Add `ExtendedFloatingActionButton` to bottom-right of the screen:
    - Use:
      ```xml
      android:layout_gravity="bottom|end"
      android:layout_marginBottom="32dp"
      android:layout_marginEnd="32dp"
      ```
    - Set icon: `@drawable/ic_calc_icon`
    - Set label: `"Calculator"`
    - Ensure onClick opens the main calculator activity

---

### 6. ♻️ Reuse Top App Bar Design
- Keep the original top bar design from the old layout:
    - `btn_back`, `screen_name`, `btn_hot_apps`, `btn_settings`
- Maintain margins, padding, and alignment
- Make sure theme colors and tinting are consistent

---

### 7. 🧠 Maintain Search Bar
- Preserve search bar structure and alignment:
    - Fixed height: `56dp`
    - Input field should be `non-editable`, and visually look like a modern search bar
    - Wrapped inside a `ConstraintLayout` to maintain alignment

---

### 8. ✨ Final Layout Visual Goals
- Home screen should feel clean, minimal, and modern.
- Visual grouping of elements must be clear:
    - History section → Grid categories → FAB
- All items should adjust to screen sizes gracefully.

---

## 🔧 Components Used

| UI Component                         | Use Case                           |
|-------------------------------------|-------------------------------------|
| `GridLayout`                        | Category buttons (3x3 grid)         |
| `MaterialCardView` / `CardView`     | Recent history item cards           |
| `ExtendedFloatingActionButton`      | Floating calculator action          |
| `ConstraintLayout`                  | Search bar container                |
| `ScrollView`                        | Main vertical scroll container      |
| `HorizontalScrollView` (or RecyclerView) | Scrollable history bar         |

---

## 🧪 Testing & Compatibility

- [ ] Works on small and large screen phones
- [ ] Buttons respond to click listeners
- [ ] Scroll behavior smooth and lag-free
- [ ] All category buttons visible and accessible
- [ ] No clipping or overlap of multi-line button labels

---

## 🗂️ Affected Files

- `res/layout/layout_home.xml`
- `MainActivity.kt` (for FAB click handling)
- Optional: `res/values/dimens.xml`, `res/values/styles.xml`

---

