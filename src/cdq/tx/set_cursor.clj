(ns cdq.tx.set-cursor
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [clojure.graphics :as graphics]))

(defn do! [cursor]
  (graphics/set-cursor! (utils/safe-get ctx/cursors cursor)))
