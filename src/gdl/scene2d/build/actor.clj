(ns gdl.scene2d.build.actor
  (:require [com.badlogic.gdx.scenes.scene2d.actor]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d :as scene2d]))

(defmethod scene2d/build :actor.type/actor [{:keys [actor/act
                                                    actor/draw]
                                             :as opts}]
  (doto (com.badlogic.gdx.scenes.scene2d.actor/create
         {:actor/act (fn [this delta]
                       (when act
                         (actor/act! this delta act)))
          :actor/draw (fn [this _batch _parent-alpha]
                        (when draw
                          (actor/draw! this draw)))})
    (actor/set-opts! opts)))
