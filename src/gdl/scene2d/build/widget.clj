(ns gdl.scene2d.build.widget
  (:require [com.badlogic.gdx.scenes.scene2d.ui.widget :as widget]
            [clojure.scene2d.actor]
            [gdl.scene2d :as scene2d]))

(defmethod scene2d/build :actor.type/widget [opts]
  (widget/create
   {:actor/draw (fn [actor _batch _parent-alpha]
                  (clojure.scene2d.actor/draw! actor (:actor/draw opts)))}))
