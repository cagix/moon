(ns cdq.body)

(defprotocol Body
  (overlaps? [_ other-body])
  (touched-tiles [_])
  (distance [_ other-body]))
