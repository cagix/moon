(ns cdq.ui.action-bar)

(defprotocol ActionBar
  (selected-skill [_])
  (add-skill! [_ {:keys [skill-id texture-region tooltip-text]}])
  (remove-skill! [_ skill-id]))

