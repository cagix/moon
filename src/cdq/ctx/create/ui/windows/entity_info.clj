(ns cdq.ctx.create.ui.windows.entity-info
  (:require [cdq.ui :as ui]
            [cdq.world.info :as info]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.scene2d.vis-ui.window :as window]
            [clojure.vis-ui.label :as label])
  (:import (com.badlogic.gdx.scenes.scene2d Group)
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
        label (label/create "")
        window (window/create {:title title
                               :actor/name actor-name
                               :actor/visible? visible?
                               :actor/position position
                               :rows [[{:actor label
                                        :expand? true}]]})]
    (.addActor window
               (actor/create
                {:act (fn [this _delta]
                        (when-let [stage (actor/stage this)]
                          (.setText label (str (set-label-text! (.ctx stage)))))
                        (.pack window))
                 :draw (fn [this batch parent-alpha])}))
    window))
