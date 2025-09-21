(ns cdq.info)

(defprotocol InfoText
  (generate [_ entity ctx]))
