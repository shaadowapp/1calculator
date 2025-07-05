✅ Bottom Navigation Bar Design & Interaction Plan
Design a custom bottom navigation bar with 3 tabs: Home, Voice (Ask Mathly), and Category, using a clean and minimalist layout. The main background of the screen stays #121212 (pure dark). Each icon+label should sit inside its own fully rounded rectangle container. When a tab is active, this rectangle changes to a very light tint of the brand color (#5D2DC8), and the icon + label color also changes to the brand color. Inactive tabs remain neutral gray.

🧩 Structural Planning:
Parent Layout: LinearLayout or ConstraintLayout at the bottom with 3 child tabs evenly spaced.

Each Tab: A custom LinearLayout (or FrameLayout) with:

A fully rounded rectangle background (initially transparent)

Icon at the top, Label below it

🎨 Styling Behavior:
Tab State	Icon Color	Label Color	Background (Rounded Box)
Active	#5D2DC8	#5D2DC8	Very light brand tint (e.g. #261C43)
Inactive	#AAAAAA	#AAAAAA	Transparent

🔁 Interaction Logic:
On tab click:

Remove active state from previous tab

Set clicked tab as active:

Change icon and label color to brand

Apply rounded rectangle background tint