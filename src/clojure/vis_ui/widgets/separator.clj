(ns clojure.vis-ui.widgets.separator
  "A separator widget (horizontal or vertical bar) that can be used in menus, tables or other widgets, typically added to new row with growX() (if creating horizontal separator) OR growY() (if creating vertical separator) PopupMenu and VisTable provides utilities addSeparator() methods that adds new separator.


  `extends com.badlogic.gdx.scenes.scene2d.ui.Widget`"
  (:import (com.kotcrab.vis.ui.widget Separator)))

(defn horizontal []
  (Separator. "default"))

(defn vertical []
  (Separator. "vertical"))
