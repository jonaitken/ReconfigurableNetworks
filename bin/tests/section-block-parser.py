#!/usr/bin/python

import unittest
import sys
sys.path.append('../')
import pynt.protocols.tl1 as tl1

class TestParseSectionBlock(unittest.TestCase):
    def test_ParseSectionBlock(self):
        """ Test the ParseSectionBlock function
        """
        PSB_test = [
                {
                'string': r'value1=test1,value2=test2,value3=test3',
                'values': {'value1': 'test1', 'value2': 'test2', 'value3': 'test3'}
                },
                {
                'string': r'TYPE=TRFC,NAME=\"Test customer, Cupertino\",PORTS=ETH-3&HSL-10,UPORTS=,SPORTS=',
                'values': {'uports': '', 'sports': '', 'type': 'TRFC', 'name': r'Test customer, Cupertino', 'ports': 'ETH-3&HSL-10'}
                },
                {
                'string': r'NAME=Test\,value1=test1',
                'values': {'name': 'Test\\', 'value1': 'test1'}
                }
                ]
        for e in PSB_test:
            res = tl1.ParseSectionBlock(e['string'])
            self.assertEqual(res, e['values'], 'Incorrectly parsed section block, expected: %s  output: %s' % (e['values'], res))


if __name__ == '__main__':
    unittest.main()
