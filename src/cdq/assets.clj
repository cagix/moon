(ns cdq.assets)

(defprotocol Assets
  (all-of-type [_ asset-type]))
