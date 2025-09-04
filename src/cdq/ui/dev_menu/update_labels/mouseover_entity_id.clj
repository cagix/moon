(ns cdq.ui.dev-menu.update-labels.mouseover-entity-id)

(defn create [icon]
  {:label "Mouseover-entity id"
   :update-fn (fn [{:keys [ctx/mouseover-eid]}]
                (when-let [entity (and mouseover-eid @mouseover-eid)]
                  (:entity/id entity)))
   :icon icon})
