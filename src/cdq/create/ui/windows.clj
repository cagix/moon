(ns cdq.create.ui.windows)

(defn create [ctx actors]
  {:actor/type :actor.type/group
   :actor/name "cdq.ui.windows"
   :group/actors (for [f actors]
                   (f ctx))})
