(ns cdq.ui.message)

(defprotocol Message
  (show! [_ text]))
