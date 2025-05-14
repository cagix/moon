(ns cdq.game.load-schemas
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn do! [path]
  (->> path
       io/resource
       slurp
       edn/read-string
       (utils/bind-root #'ctx/schemas)))
