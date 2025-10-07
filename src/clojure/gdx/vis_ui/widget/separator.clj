(ns clojure.gdx.vis-ui.widget.separator
  (:import (com.kotcrab.vis.ui.widget Separator)))

(defn horizontal []
  (Separator. "default"))

(defn vertical []
  (Separator. "vertical"))
