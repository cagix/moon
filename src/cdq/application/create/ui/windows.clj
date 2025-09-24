(ns cdq.application.create.ui.windows
  (:require cdq.application.create.ui.entity-info
            cdq.application.create.ui.inventory))

(defn create [ctx]
  {:actor/type :actor.type/group
   :actor/name "cdq.ui.windows"
   :group/actors (for [f [cdq.application.create.ui.entity-info/create
                          cdq.application.create.ui.inventory/create]]
                   (f ctx))})
