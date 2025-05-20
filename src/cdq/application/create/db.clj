(ns cdq.application.create.db
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.utils :refer [bind-root]]))

(defn do! []
  (bind-root #'ctx/db (db/create (:db ctx/config)
                                 (:schemas ctx/config))))
