(ns cdq.ctx)

(defprotocol InfoText ; OK !?
  (info-text [_ object]))

(defprotocol Editor ; # WTF # 2
  (open-property-editor-window! [_ property])
  (open-editor-overview-window! [_ property-type]))

(defprotocol InteractionState
  (interaction-state [_ player-eid])) ; WTF # 1
