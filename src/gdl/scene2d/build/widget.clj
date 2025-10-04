(ns gdl.scene2d.build.widget
  (:require [clojure.gdx.scenes.scene2d.ui.widget :as widget]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.actor :as actor]))

(defmethod scene2d/build :actor.type/widget [opts]
  (widget/create
   {:actor/draw (fn [actor _batch _parent-alpha]
                  (actor/draw! actor (:actor/draw opts)))}))
