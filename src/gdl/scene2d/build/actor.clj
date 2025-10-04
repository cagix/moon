(ns gdl.scene2d.build.actor
  (:require [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            clojure.scene2d.actor
            [gdl.scene2d.actor]
            [gdl.scene2d :as scene2d]))

(defmethod scene2d/build :actor.type/actor [{:keys [actor/act
                                                    actor/draw]
                                             :as opts}]
  (doto (actor/create
         {:actor/act (fn [this delta]
                       (when act
                         (clojure.scene2d.actor/act! this delta act)))
          :actor/draw (fn [this _batch _parent-alpha]
                        (when draw
                          (clojure.scene2d.actor/draw! this draw)))})
    (gdl.scene2d.actor/set-opts! opts)))
