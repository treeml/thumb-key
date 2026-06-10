// Hardcoded seed books for Discover shelves.
// These load instantly without any network call. API results replace them
// in the background when available.
function gb(id, title, author, subjects = []) {
  return {
    id,
    title,
    authors:       [{ name: author }],
    formats: {
      'image/jpeg':               `https://www.gutenberg.org/cache/epub/${id}/pg${id}.cover.medium.jpg`,
      'text/plain; charset=utf-8': `https://www.gutenberg.org/cache/epub/${id}/pg${id}.txt`,
    },
    subjects,
    languages:     ['en'],
    download_count: 0,
    source:        'gutenberg',
  }
}

export const SEEDS = {
  popular: [
    gb(1342,  'Pride and Prejudice',              'Austen, Jane'),
    gb(84,    'Frankenstein',                      'Shelley, Mary Wollstonecraft'),
    gb(1661,  'The Adventures of Sherlock Holmes', 'Doyle, Arthur Conan'),
    gb(11,    "Alice's Adventures in Wonderland",  'Carroll, Lewis'),
    gb(2701,  'Moby Dick',                         'Melville, Herman'),
    gb(345,   'Dracula',                           'Stoker, Bram'),
    gb(174,   'The Picture of Dorian Gray',        'Wilde, Oscar'),
    gb(98,    'A Tale of Two Cities',              'Dickens, Charles'),
    gb(2554,  'Crime and Punishment',              'Dostoyevsky, Fyodor'),
    gb(768,   'Wuthering Heights',                 'Brontë, Emily'),
  ],
  adventure: [
    gb(120,   'Treasure Island',                              'Stevenson, Robert Louis'),
    gb(521,   'Robinson Crusoe',                              'Defoe, Daniel'),
    gb(1184,  'The Count of Monte Cristo',                    'Dumas, Alexandre'),
    gb(164,   'Twenty Thousand Leagues under the Seas',       'Verne, Jules'),
    gb(76,    'Adventures of Huckleberry Finn',               'Twain, Mark'),
    gb(74,    'The Adventures of Tom Sawyer',                 'Twain, Mark'),
    gb(103,   'Around the World in 80 Days',                  'Verne, Jules'),
    gb(2226,  'Twenty Years After',                           'Dumas, Alexandre'),
  ],
  mystery: [
    gb(1661,  'The Adventures of Sherlock Holmes', 'Doyle, Arthur Conan'),
    gb(2852,  'The Hound of the Baskervilles',     'Doyle, Arthur Conan'),
    gb(244,   'A Study in Scarlet',                'Doyle, Arthur Conan'),
    gb(2097,  'The Sign of the Four',              'Doyle, Arthur Conan'),
    gb(155,   'The Moonstone',                     'Collins, Wilkie'),
    gb(3289,  'The Mystery of the Yellow Room',    'Leroux, Gaston'),
    gb(863,   'The Mysterious Affair at Styles',   'Christie, Agatha'),
  ],
  romance: [
    gb(1342,  'Pride and Prejudice',   'Austen, Jane'),
    gb(1260,  'Jane Eyre',             'Brontë, Charlotte'),
    gb(161,   'Sense and Sensibility', 'Austen, Jane'),
    gb(768,   'Wuthering Heights',     'Brontë, Emily'),
    gb(158,   'Emma',                  'Austen, Jane'),
    gb(105,   'Persuasion',            'Austen, Jane'),
    gb(9296,  'North and South',       'Gaskell, Elizabeth'),
  ],
  scifi: [
    gb(36,    'The War of the Worlds',            'Wells, H. G.'),
    gb(35,    'The Time Machine',                 'Wells, H. G.'),
    gb(84,    'Frankenstein',                     'Shelley, Mary Wollstonecraft'),
    gb(3748,  'Journey to the Centre of the Earth', 'Verne, Jules'),
    gb(159,   'The Island of Doctor Moreau',      'Wells, H. G.'),
    gb(5230,  'The First Men in the Moon',        'Wells, H. G.'),
    gb(164,   'Twenty Thousand Leagues under the Seas', 'Verne, Jules'),
  ],
  philosophy: [
    gb(2680,  'Meditations',              'Aurelius, Marcus'),
    gb(1497,  'The Republic',             'Plato'),
    gb(1998,  'Thus Spoke Zarathustra',   'Nietzsche, Friedrich'),
    gb(4363,  'Beyond Good and Evil',     'Nietzsche, Friedrich'),
    gb(1232,  'The Prince',               'Machiavelli, Niccolò'),
    gb(6343,  'On Liberty',               'Mill, John Stuart'),
  ],
  history: [
    gb(731,   'The History of the Decline and Fall of the Roman Empire', 'Gibbon, Edward'),
    gb(1232,  'The Prince',               'Machiavelli, Niccolò'),
    gb(2680,  'Meditations',              'Aurelius, Marcus'),
    gb(17405, 'The Art of War',           'Sun Tzu'),
    gb(148,   'Autobiography of Benjamin Franklin', 'Franklin, Benjamin'),
    gb(2500,  "Caesar's Gallic War",      'Caesar, Julius'),
  ],
  poetry: [
    gb(1322,  'Leaves of Grass',                    'Whitman, Walt'),
    gb(20,    'Paradise Lost',                       'Milton, John'),
    gb(12242, 'Poems by Emily Dickinson',            'Dickinson, Emily'),
    gb(8800,  'The Divine Comedy',                   'Dante Alighieri'),
    gb(1934,  'Songs of Innocence and Experience',   'Blake, William'),
    gb(19,    'The Raven and Other Poems',           'Poe, Edgar Allan'),
  ],
}
