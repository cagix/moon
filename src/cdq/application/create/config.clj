(ns cdq.application.create.config
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root
                               io-slurp-edn
                               safe-get]]))

(defn do! []
  (bind-root #'ctx/config (let [m (io-slurp-edn "config.edn")]
                            (reify clojure.lang.ILookup
                              (valAt [_ k]
                                (safe-get m k))))))
