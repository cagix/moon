(ns cdq.schema.enum
  (:require [cdq.schema :as schema]
            [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.vis-ui.select-box :as select-box]))

(defmethod schema/malli-form :s/enum [[_ & params] _schemas]
  (apply vector :enum params))

(defn create [schema _attribute v _ctx]
  {:actor/type :actor.type/select-box
   :items (map utils/->edn-str (rest schema))
   :selected (utils/->edn-str v)})

(defn value [_  _attribute widget _schemas]
  (edn/read-string (select-box/get-selected widget)))
