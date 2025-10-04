(ns clojure.gdx.scenes.scene2d.vis-ui.widget.separator
  (:import (com.kotcrab.vis.ui.widget Separator)))

(defn horizontal []
  (Separator. "default"))

(defn vertical []
  (Separator. "vertical"))
