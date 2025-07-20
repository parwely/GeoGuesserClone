const express = require('express');
const router = express.Router();

// Get random locations for game
router.get('/random/:count', (req, res) => {
  const count = parseInt(req.params.count) || 5;
  res.json({ 
    message: `Get ${count} random locations - to be implemented`,
    count
  });
});

module.exports = router;