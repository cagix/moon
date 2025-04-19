(ns gdl.gdx.scenes.scene2d.ui.widget-group
  (:import (com.badlogic.gdx.scenes.scene2d.ui WidgetGroup)))

(defn pack! [this]
  (WidgetGroup/.pack this))
