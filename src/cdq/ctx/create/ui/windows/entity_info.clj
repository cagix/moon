(ns cdq.ctx.create.ui.windows.entity-info
  (:require [cdq.ui :as ui]
            [clojure.gdx.scenes.scene2d.ui.label :as label]
            [clojure.gdx.scenes.scene2d.ui.widget-group :as widget-group]
            [clojure.info :as info]
            [clojure.scene2d :as scene2d]
            [clojure.gdx.scenes.scene2d.group :as group]))

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
    (group/add-actor! window
                      (scene2d/build
                       {:actor/type :actor.type/actor
                        :actor/act (fn [_this _delta ctx]
                                     (label/set-text! label (str (set-label-text! ctx)))
                                     (widget-group/pack! window))}))
    window))
