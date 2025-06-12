(ns cdq.ui.windows)

(defn create [ctx {:keys [id actors]}]
  {:actor/type :actor.type/group
   :id id
   :actors (for [[f params] actors]
             (f ctx params))})
