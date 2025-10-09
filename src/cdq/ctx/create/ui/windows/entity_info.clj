(ns cdq.ctx.create.ui.windows.entity-info
  (:require [cdq.ui :as ui]
            [cdq.world.info :as info]
            [clojure.scene2d :as scene2d])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)
           (com.badlogic.gdx.scenes.scene2d.ui Label)))

(defn create [{:keys [ctx/stage]}]
  (let [title "info"
        actor-name "cdq.ui.windows.entity-info"
        visible? false
        position [(ui/viewport-width stage) 0]
        set-label-text! (fn [{:keys [ctx/world]}]
                          (if-let [eid (:world/mouseover-eid world)]
                            (info/text (apply dissoc @eid [:entity/skills
                                                           :entity/faction
                                                           :active-skill])
                                       world)
                            ""))
        label (scene2d/build {:actor/type :actor.type/label
                              :label/text ""})
        window (scene2d/build {:actor/type :actor.type/window
                               :title title
                               :actor/name actor-name
                               :actor/visible? visible?
                               :actor/position position
                               :rows [[{:actor label :expand? true}]]})]
    (Group/.addActor window
                     (proxy [Actor] []
                       (act [_delta]
                         (when-let [stage (.getStage this)]
                           (.setText label (str (set-label-text! (.ctx stage)))))
                         (.pack window))))
    window))
