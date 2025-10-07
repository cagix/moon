(ns clojure.scene2d.build.widget
  (:require [clojure.scene2d :as scene2d]
            [clojure.scene2d.build.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Widget)))

(defmethod scene2d/build :actor.type/widget [opts]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (actor/draw! this (:actor/draw opts)))))
