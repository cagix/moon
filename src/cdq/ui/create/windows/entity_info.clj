(ns cdq.ui.create.windows.entity-info
  (:require [cdq.ui :as ui]
            [cdq.ui.stage :as stage]
            [cdq.world.info :as info]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [clojure.gdx.scene2d.ui.label :as label]
            [clojure.gdx.scene2d.ui.widget-group :as widget-group]
            [clojure.scene2d.vis-ui.window :as window]
            [clojure.vis-ui.label :as vis-label]))

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
        label (vis-label/create "")
        window (window/create {:title title
                               :actor/name actor-name
                               :actor/visible? visible?
                               :actor/position position
                               :rows [[{:actor label
                                        :expand? true}]]})]
    (group/add-actor! window (actor/create
                              {:act (fn [this _delta]
                                      (when-let [stage (actor/stage this)]
                                        (label/set-text! label (set-label-text! (stage/ctx stage))))
                                      (widget-group/pack! window))
                               :draw (fn [this batch parent-alpha])}))
    window))
