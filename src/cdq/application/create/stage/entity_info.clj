(ns cdq.application.create.stage.entity-info
  (:require [cdq.stage :as stage]
            [cdq.info :as info]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.group :as group]
            [clojure.scene2d.ui.label :as label]
            [clojure.scene2d.ui.widget-group :as widget-group]))

(defn create [stage]
  (let [title "info"
        actor-name "cdq.ui.windows.entity-info"
        visible? false
        position [(stage/viewport-width stage) 0]
        set-label-text! (fn [{:keys [ctx/world]}]
                          (if-let [eid (:world/mouseover-eid world)]
                            (info/info-text (apply dissoc @eid [:entity/skills
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
    (group/add! window (scene2d/build
                        {:actor/type :actor.type/actor
                         :act (fn [_this _delta ctx]
                                (label/set-text! label (str (set-label-text! ctx)))
                                (widget-group/pack! window))}))
    window))
