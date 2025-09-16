(ns cdq.body)

(defprotocol Body
  (overlaps? [_ other-body])
  (touched-tiles [_]))
