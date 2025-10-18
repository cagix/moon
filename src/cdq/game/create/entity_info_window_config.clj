(ns cdq.game.create.entity-info-window-config
  (:require [cdq.ui :as ui]
            [cdq.world.info :as info]))

(defn create
  [{:keys [ctx/stage]}]
  {:title "Entity Info"
   :actor-name "cdq.ui.windows.entity-info"
   :visible? false
   :position [(ui/viewport-width stage) 0]
   :set-label-text! (fn [{:keys [ctx/world]}]
                      (if-let [eid (:world/mouseover-eid world)]
                        (info/text (apply dissoc @eid [:entity/skills
                                                       :entity/faction
                                                       :active-skill])
                                   world)
                        ""))})
