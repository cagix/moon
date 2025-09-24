(ns cdq.application.create.stage.windows
  (:require cdq.application.create.stage.entity-info
            cdq.application.create.stage.inventory))

(defn create [stage graphics]
  {:actor/type :actor.type/group
   :actor/name "cdq.ui.windows"
   :group/actors [(cdq.application.create.stage.entity-info/create stage)
                  (cdq.application.create.stage.inventory/create stage graphics)]})
