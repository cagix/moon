(ns cdq.ui.widget
  (:require [cdq.ui :as ui]
            cdq.construct)
  (:import (com.badlogic.gdx.scenes.scene2d.ui Widget)))

(defmethod cdq.construct/construct :actor.type/widget [opts]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (when-let [f (:draw opts)]
        (ui/try-draw this f)))))
