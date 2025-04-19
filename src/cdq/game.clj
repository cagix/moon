(ns cdq.game
  (:require cdq.application
            [cdq.create.db :as db]
            cdq.create.effects
            cdq.create.entity-components
            cdq.create.schemas
            cdq.render
            cdq.world.context
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn- create-game [context]
  (let [schemas (-> "schema.edn" io/resource slurp edn/read-string)
        context (merge context
                       {:cdq/db (db/create schemas)
                        :context/entity-components (cdq.create.entity-components/create)
                        :cdq/schemas schemas})]
    (cdq.world.context/reset context :worlds/vampire)))

(defn -main []
  (cdq.application/start! create-game cdq.render/game-loop!))
