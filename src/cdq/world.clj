(ns cdq.world)

(defprotocol Resettable
  (reset-state [_ world-fn-result]))

(defprotocol World
  (active-eids [_]))

(defprotocol MovementAI
  (find-movement-direction [_ eid]))

(defprotocol RayCaster
  (ray-blocked? [_ start target])
  (path-blocked? [_ start target path-w])
  (line-of-sight? [_ source target]))

(defprotocol InfoText
  (info-text [_ entity]))

(defprotocol FSMs
  (handle-event [_ eid event]
                [_ eid event params]))

(defprotocol Update
  (update-potential-fields! [_])
  (update-time [_ delta-ms]))
