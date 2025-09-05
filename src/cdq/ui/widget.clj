(ns cdq.ui.widget
  (:require [cdq.ui :as ui]
            [clojure.gdx.scenes.scene2d.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Widget)))

(defmethod actor/construct :actor.type/widget [opts]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (when-let [f (:draw opts)]
        (ui/try-draw this f)))))
