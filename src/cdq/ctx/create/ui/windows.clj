(ns cdq.ctx.create.ui.windows
  (:require [cdq.ui.build.group :as group]))

(defn create [ctx actor-fns]
  (group/create
   {:actor/name "cdq.ui.windows"
    :group/actors (for [[actor-fn & params] actor-fns]
                    (apply (requiring-resolve actor-fn) ctx params))}))
