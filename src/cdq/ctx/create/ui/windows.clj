(ns cdq.ctx.create.ui.windows)

(defn create [ctx actor-fns]
  {:actor/type :actor.type/group
   :actor/name "cdq.ui.windows"
   :group/actors (for [[actor-fn & params] actor-fns]
                   (apply (requiring-resolve actor-fn) ctx params))})
