(ns cdq.application.create.stage.windows)

(defn create [stage graphics]
  {:actor/type :actor.type/group
   :actor/name "cdq.ui.windows"
   :group/actors [((requiring-resolve 'cdq.application.create.stage.entity-info/create) stage)
                  ((requiring-resolve 'cdq.application.create.stage.inventory/create) stage graphics)]})
