(ns cdq.ui.dev-menu.update-labels.mouseover-entity-id)

(defn create [icon]
  {:label "Mouseover-entity id"
   :update-fn (fn [{:keys [ctx/world]}]
                (let [eid (:world/mouseover-eid world)]
                  (when-let [entity (and eid @eid)]
                    (:entity/id entity))))
   :icon icon})
