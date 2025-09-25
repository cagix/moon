(ns gdl.plattform)

(defprotocol Plattform
  (stage [_ viewport batch]))
