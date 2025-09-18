(ns com.kotcrab.vis.ui.widget.menu-item
  (:import (com.kotcrab.vis.ui.widget MenuItem)))

(defn create [label]
  (MenuItem. label))
