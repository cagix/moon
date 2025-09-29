(ns clojure.scene2d.ui.widget
  (:require [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Widget)))

(defmethod scene2d/build :actor.type/widget
  [opts]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (when-let [f (:draw opts)]
        (actor/draw this f)))))

(defn set-opts! [actor opts]
  (actor/set-opts! actor opts))
